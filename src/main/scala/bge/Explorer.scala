import actions._
import util._
import sys.process._
import collection.mutable.Map

/**
 * Created with IntelliJ IDEA.
 * User: yzark
 * Date: 11/19/1
 * Time: 12:36 PM
 * To change this template use File | Settings | File Templates.
 */
object Explorer extends App with db.BitcoinDB {
  args.toList match{
    case "start"::rest =>

      // Ensure that bitcoind is running
      // Seq("bitcoind","-daemon").run
      // now commented out because bitcoind isn't available on mac by default
      // just start bitcoind -daemon manually

      populate

      Seq("touch",lockFile).!
      iterateResume

    case "populate"::rest             =>

      
      populate

    case "resume"::rest =>

      iterateResume
      
    case "info"::rest =>

      getInfo

    case _=>

      println("""

        Available commands:

         start: populate, then resume 
         populate: create the database movements with movements and closures.
         resume: update the database generated by populate with new incoming data.

      """)
  }

  def getInfo = {
    val (count, amount) = sumUTXOs
    println("Sum of the utxos saved in the lmdb: "+ amount)
    println("Total utxos in the lmdb: " + count)
    val (countDB, amountDB) = countUTXOs
    println("Sum of the utxos in the sql db " +amountDB)
    println("Total utxos in the sql db " + countDB)
  }

  def totalExpectedSatoshi(blockCount: Int): Long = {
    val epoch = blockCount/210000
    val blocksSinceEpoch = blockCount % 210000
    def blockReward(epoch: Int) = Math.floor(50L*100000000L/Math.pow(2,epoch)).toLong
    val fullEpochs = for (i <- (0 until epoch))
                     yield 210000L * blockReward(i)
    val correct = fullEpochs.sum + blocksSinceEpoch * blockReward(epoch)
    correct - blockReward(0) * (if (blockCount > 91880) 2 else if (blockCount > 191842) 1 else 0)
    // correct for the two duplicate coinbase tx (see BIP 30) that we just store once (they are unspendable anyway)
  }

  def sumUTXOs = {
    lazy val table = LmdbMap.open("utxos")
    lazy val outputMap: UTXOs = new UTXOs (table)
    // (txhash,index) -> (address,value,blockIn)
    val values = for ( (_,(_,value,_)) <- outputMap.view) yield value //makes it a lazy collection
    val tuple = values.grouped(100000).foldLeft((0,0L)){
      case ((count,sum),group) =>
        println(count + " elements read at " + java.util.Calendar.getInstance().getTime())
        val seq = group.toSeq
        (count+seq.size,sum+seq.sum)
    }

    table.close
    tuple
  }

  def populate = {

    val dataDirectory = new java.io.File(dataDir)

    if (!dataDirectory.isDirectory)
      dataDirectory.mkdir

    initializeReaderTables
    initializeClosureTables
    initializeStatsTables

    insertStatistics
 
    PopulateBlockReader
  
    createIndexes
    new PopulateClosure(PopulateBlockReader.processedBlocks)
    createAddressIndexes    
    populateStats
//    testValues

  }

  def resume = {
    val read = new ResumeBlockReader
    val closure = new ResumeClosure(read.processedBlocks)
    println("DEBUG: making new stats")
    resumeStats(read.changedAddresses, closure.changedReps, closure.addedAds, closure.addedReps)
  }

  def iterateResume = {
    // Seq("bitcoind","-daemon").run
    
    if (!peerGroup.isRunning) startBitcoinJ

    //val lch = lastCompletedHeight
    //val bc = blockCount

    // if there are more stats than blocks we could delete it
    //for (i <- (lch +1 until bc).reverse){
    //  println("rolling back block " + i + " at " + java.util.Calendar.getInstance().getTime())
    //  rollBack
    // }
    //if (lch < bc-1){
    //  println(lch + " - " + bc)
    //  populateStats
    //}
    while (new java.io.File(lockFile).exists)
    {
//      val from = blockCount
  //    val to = countLines(blockHashListFile)
      
    //  if (to > from)
    //  {
    //  println("Reading blocks from " + from + " until " + to)

      if (blockCount > chain.getBestChainHeight-5)
      {
        println("waiting for new blocks at " + java.util.Calendar.getInstance().getTime())
        chain.getHeightFuture(blockCount+5).get //wait until the chain is at least 6 blocks longer than we have read
      }

      //testValues
      resume
               

     // }
     // else
     // {
     //   println("waiting for new blocks")
     //   waitIfNewBlocks(to)
     // }
    }
    println("process stopped")
    //Seq("bitcoin-cli","stop").run
  }

  def testValues = {
    
    val (count,amount) = sumUTXOs
    val (countDB, amountDB) = countUTXOs
    val expected = totalExpectedSatoshi(blockCount)
    assert( count == countDB, "count differs " + count + "!="+countDB)
    assert(amount == amountDB, "amount differs " + amount + " != " + amountDB)
    assert(amount <= expected, "It should be " + expected + " satoshis but we have " + ((amount-expected)/100000000.0) + " too many bitcoins")
    println("Values seem to be correct.")
  }

  def resumeStats(changedAddresses: Map[Hash,Long], changedReps: Map[Hash,Set[Hash]], addedAds: Int, addedReps: Int)  = {
    
    println("DEBUG: "+ changedAddresses.size + " addresses changed balance")

    if (changedAddresses.size < 38749 )
    {
      updateBalanceTables(changedAddresses, changedReps)
      insertRichestAddresses
      insertRichestClosures
      updateStatistics(changedReps,addedAds, addedReps)
    }
    else populateStats
        
  }

  def populateStats = {
    createBalanceTables
    insertRichestAddresses
    insertRichestClosures
    insertStatistics
  }


}

 with import <nixpkgs> {};
 stdenv.mkDerivation {
    name = "bge";

    buildPhase = let
      sbtBootDir = "./.sbt/boot/";
      sbtIvyHome = "/var/tmp/`whoami`/.ivy";
      sbtOpts = "-XX:PermSize=190m -Dsbt.boot.directory=${sbtBootDir} -Dsbt.ivy.home=${sbtIvyHome}";
      in ''
        cd api
        mkdir -p ${sbtBootDir}
        mkdir -p ${sbtIvyHome}
        sbt ${sbtOpts} assembly
       '';

    installPhase = ''
      mkdir -p $out
      cp  ./target/scala-2.11/bgeapi-assembly-1.0.jar $out
      cp bgeapi $out
    '';

    src = fetchgit {
    url = "git://github.com/bitcoinprivacy/Bitcoin-Graph-Explorer.git";
    rev = "HEAD";
    md5 = "5258472d38d4e985b91577b26ad37646";
    } ;
    JAVA_HOME = "${jdk}";
    shellHook = ''

    export PS1="BGE > " '';
    LD_LIBRARY_PATH="${stdenv.cc.cc}/lib64";

    buildInputs = [sbt];
}

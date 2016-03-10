if [ -z "$1" ] || [ -z "$2" ] || [ -z "$3" ]
then
  echo "Usage: $0 <host> <sslport> <domain>"
  exit
fi

mvn exec:java -Dcustom.props=./src/main/resources/jacorb.props -Dexec.mainClass=demo.HelloClient -Dexec.args="$1 $2 demo_hello_java_client demo_hello_java_client $3"

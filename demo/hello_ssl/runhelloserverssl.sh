if [ -z "$1" ] || [ -z "$2" ]
then
  echo "Usage: $0 <host> <sslport>"
  exit
fi

mvn exec:java -Dcustom.props=./src/main/resources/jacorb.props -Dexec.mainClass=demo.HelloServer -Dexec.args="$1 $2 demo_hello_java admin/DemoHello.key"

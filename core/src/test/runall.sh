#!/bin/bash

mode=$1

if [[ "$mode" == "" ]]; then
	mode=RELEASE
elif [[ "$mode" != "RELEASE" && "$mode" != "DEBUG" ]]; then
	echo "Usage: $0 <RELEASE|DEBUG>"
	exit 1
fi

bus1port=21220
leasetime=5
passwordpenalty=1

export OPENBUS_TESTCFG=$OPENBUS_TEMP/test.properties
echo "bus.host.port=$bus1port"                 > $OPENBUS_TESTCFG
echo "login.lease.time=$leasetime"            >> $OPENBUS_TESTCFG
echo "password.penalty.time=$passwordpenalty" >> $OPENBUS_TESTCFG
#echo "openbus.test.verbose=yes"               >> $OPENBUS_TESTCFG

source ${OPENBUS_CORE_TEST}/runbus.sh $mode BUS01 $bus1port
#genkey $OPENBUS_TEMP/testsyst

# go to resources, where the scipt is located
pushd resources
source ${OPENBUS_CORE_TEST}/runadmin.sh $mode localhost $bus1port --script=sdk_java_test.adm

echo "bus.reference.path = $OPENBUS_TEMP/BUS01.ior" > test.properties
echo "bus.host.name = localhost" >> test.properties
echo "bus.host.port = $bus1port" >> test.properties
echo "admin.entity.name = admin" >> test.properties
echo "admin.password = admin" >> test.properties
echo "user.entity.name = tester" >> test.properties
echo "user.password = tester" >> test.properties
echo "user.password.domain = testing" >> test.properties
echo "system.entity.name = TestEntity" >> test.properties
echo "system.private.key = src/test/resources/TestEntity.key" >> test.properties
echo "system.wrong.key = src/test/resources/wrongkey.key" >> test.properties
echo "system.wrong.name = NoCertServer" >> test.properties
echo "openbus.log.level = OFF" >> test.properties

# go to core/pom.xml
pushd ../../..
mvn test
assert_ok $?
# back to resources
popd
source ${OPENBUS_CORE_TEST}/runadmin.sh $mode localhost $bus1port --undo-script=sdk_java_test.adm


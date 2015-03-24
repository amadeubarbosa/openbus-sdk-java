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

# go to resources, where the script is located
pushd resources
source ${OPENBUS_CORE_TEST}/runadmin.sh $mode localhost $bus1port --script=sdk_java_test.adm

echo "bus.reference.path = $OPENBUS_TEMP/BUS01.ior" > $OPENBUS_TESTCFG
echo "bus.host.name = localhost" >> $OPENBUS_TESTCFG
echo "bus.host.port = $bus1port" >> $OPENBUS_TESTCFG
echo "admin.entity.name = admin" >> $OPENBUS_TESTCFG
echo "admin.password = admin" >> $OPENBUS_TESTCFG
echo "user.entity.name = tester" >> $OPENBUS_TESTCFG
echo "user.password = tester" >> $OPENBUS_TESTCFG
echo "user.password.domain = testing" >> $OPENBUS_TESTCFG
echo "system.entity.name = TestEntity" >> $OPENBUS_TESTCFG
echo "system.private.key = src/test/resources/TestEntity.key" >> $OPENBUS_TESTCFG
echo "system.wrong.key = src/test/resources/wrongkey.key" >> $OPENBUS_TESTCFG
echo "system.wrong.name = NoCertServer" >> $OPENBUS_TESTCFG
echo "openbus.log.level = 0" >> $OPENBUS_TESTCFG

# go to core/pom.xml
pushd ../../..
mvn test
assert_ok $?
# back to resources
popd
source ${OPENBUS_CORE_TEST}/runadmin.sh $mode localhost $bus1port --undo-script=sdk_java_test.adm


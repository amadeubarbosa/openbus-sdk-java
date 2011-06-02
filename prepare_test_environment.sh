#!/bin/bash

if [ $# -lt 1 ]
then
  echo "Use: `basename $0` <admin_user> [management_options]"
  exit
fi

admin_user=$1
shift

if [ ! -f ${OPENBUS_HOME}/data/certificates/AccessControlService.crt ]
then
  echo "O certificado digital do barramento \
${OPENBUS_HOME}/data/certificates/AccessControlService.crt \
n�o foi encontrado."
  exit
fi

if [ ! -f ${OPENBUS_HOME}/bin/openssl-generate.ksh ]
then
  echo "O script para gera��o de certificados \
${OPENBUS_HOME}/bin/openssl-generate.ksh \
n�o foi encontrado."
  exit
fi

if [ ! -f ${OPENBUS_HOME}/core/bin/run_management.sh ]
then
  echo "O script de governan�a do barramento \
${OPENBUS_HOME}/core/bin/run_management.sh \
n�o foi encontrado."
  exit
fi

(
cd core/test/resources;
cp ${OPENBUS_HOME}/data/certificates/AccessControlService.crt openbus.crt;
${OPENBUS_HOME}/bin/openssl-generate.ksh -n sdk_java_core;
keytool -v -import -alias openbus_alias -file openbus.crt -keypass ABCDEF -keystore keystore -storepass 123456
keytool -v -genkey -alias sdk_java_core_alias -keyalg RSA -keysize 2048 -keypass ABCDEF -keystore keystore -storepass 123456
keytool -v -export -alias sdk_java_core_alias -keystore keystore -file sdk_java_core_jks.crt -storepass 123456
)

(
cd integration_test/test/resources;
cp ${OPENBUS_HOME}/data/certificates/AccessControlService.crt openbus.crt;
${OPENBUS_HOME}/bin/openssl-generate.ksh -n sdk_java_integration
)

${OPENBUS_HOME}/core/bin/run_management.sh --login=$admin_user --script=sdk_java_test.mgt $@

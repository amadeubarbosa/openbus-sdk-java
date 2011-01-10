#!/bin/bash

if [ $# -ne 1 ]
then
  echo "Use: `basename $0` <admin_user>"
  exit
fi

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

(cd core/test/resources;
cp ${OPENBUS_HOME}/data/certificates/AccessControlService.crt openbus.crt;
${OPENBUS_HOME}/bin/openssl-generate.ksh -n sdk_java_core)

(cd integration_test/test/resources;
cp ${OPENBUS_HOME}/data/certificates/AccessControlService.crt openbus.crt;
${OPENBUS_HOME}/bin/openssl-generate.ksh -n sdk_java_integration)

${OPENBUS_HOME}/core/bin/run_management.sh --login=$1 --script=sdk_java.mgt

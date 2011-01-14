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
não foi encontrado."
  exit
fi

if [ ! -f ${OPENBUS_HOME}/bin/openssl-generate.ksh ]
then
  echo "O script para geração de certificados \
${OPENBUS_HOME}/bin/openssl-generate.ksh \
não foi encontrado."
  exit
fi

if [ ! -f ${OPENBUS_HOME}/core/bin/run_management.sh ]
then
  echo "O script de governança do barramento \
${OPENBUS_HOME}/core/bin/run_management.sh \
não foi encontrado."
  exit
fi

(cd hello/resources;
cp ${OPENBUS_HOME}/data/certificates/AccessControlService.crt openbus.crt;
${OPENBUS_HOME}/bin/openssl-generate.ksh -n sdk_java_demo_hello)

(cd delegate/resources;
cp ${OPENBUS_HOME}/data/certificates/AccessControlService.crt openbus.crt;
${OPENBUS_HOME}/bin/openssl-generate.ksh -n sdk_java_demo_delegate_client;
${OPENBUS_HOME}/bin/openssl-generate.ksh -n sdk_java_demo_delegate_server)

${OPENBUS_HOME}/core/bin/run_management.sh --login=$1 --script=sdk_java_demo.mgt


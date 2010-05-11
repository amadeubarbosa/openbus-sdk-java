#!/bin/ksh

if [ -z $1 ]; then
   echo "Você precisa especificar a quantidade de usuarios a simular"
   exit
else
  for (( i=1; i<=$1; i++ ))
  do
     (mvn test > testOutput/mvn_test_output[$i].txt &)
  done 
  wait
fi

exit



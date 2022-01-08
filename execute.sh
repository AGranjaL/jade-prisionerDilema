export CLASSPATH=$CLASPATH:$1
cd src/jade
javac -cp $1 *java
java jade.Boot -nomtp -gui -agents ${2}


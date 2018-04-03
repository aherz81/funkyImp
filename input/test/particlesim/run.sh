#!/bin/bash
# Execute C
num=256
it=2000
runs=10
destdir=`pwd`/output.csv
rm -rf output.csv
#DEFINES
# BUILD
echo "BUILDING C"
	cd C/
	echo "#define NUM_ATOMS "$num"
#define NUM_ITERATIONS "$it | cat - Defines_temp.h > Defines.h
	make clean
	make > /dev/null 2>&1
# RUN
a=0
echo "READY C"
sleep 5
echo "RUN C"
while [ $a -lt $runs ]
do
	before=$(date +%s%N | cut -b1-13)
	./example.o > /dev/null
	after=$(date +%s%N | cut -b1-13)
	if [ $a -eq 0 ]
		then
			echo -n $((after - $before))>> "$destdir"
	else
		echo -n ","$((after - $before))>> "$destdir"
	fi
	echo "RUN "$a": "$((after - $before))" ms"
	a=`expr $a + 1`
done
	echo "" >> "$destdir"
echo "FINISHED C"
sleep 5

# Execute FUNKY
#BUILD
#DEFINES
sq=`expr $num \* $num`
echo "BUILDING FUNKY"
	cd ../funky
	echo "#define NUM_ATOMS_SQUARE "$sq "
#define NUM_ATOMS "$num"
#define NUM_ITERATIONS "$it | cat - atoms.f > temp.f
	make clean
	make > /dev/null 2>&1
## RUN
b=0
echo "READY FUNKY"
sleep 5
echo "RUN FUNKY"
while [ $b -lt $runs ]
do
	tmp/cur.run > output
	tmp=`cat output | tail -n 1 | grep "time: " | sed -e 's/time: \([0-9]*\.[0-9]*\) .*\[s\].*/\1/g'`
	value=`echo $tmp \\* 1000 | bc`
	dur=`echo $value | cut -d'.' -f 1`
	if [ $b = 0 ]
		then
			echo -n $dur >> "$destdir"
	else
		echo -n ','$dur >> "$destdir"
	fi
	echo -n "RUN "$b": "$dur" ms     "
  find=`cat output | grep retval`
	if [ "$find" = "" ]
	then
		echo "FAILED"
	else
		echo "SUCCESS"
	fi
	b=`expr $b + 1`
done
#rm -rf output
# PRINT
cd ..
python box.py

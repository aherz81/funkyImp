res=`cmp $1 ./tmp/cmp.dot`

if [ "$res" = "" ]
then
	exit 0
else
	echo "output files differ : $res"
  exit 1
fi


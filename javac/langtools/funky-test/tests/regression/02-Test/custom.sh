echo "I'm a custom test, I always fail"

find=`grep -n "Local Width" $2`

#echo $find

if [ "$find" = "" ]
then
	echo "failed to find 'Local Width FAIL' in '$2', which is ok because it shouldn't be in there" 
	#exit 1 #actually..we don't fail..but in order to fail you would exit 1 here!
	exit 0
else
  exit 0
fi


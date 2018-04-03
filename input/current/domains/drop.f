package domains;

domain drop{x}(m):one_d{x-m}(b) = { one_d{x}(a) | a >= m & b=a & m>=0 }


//domain my_first_domain<a,b>(i):linear<a> = { (i) | i < a+b & i > 2 }





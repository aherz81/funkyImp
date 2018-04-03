package domains.linalg;

import domains.two_d;

domain col{x,y}(r):one_d{x}(s) = { two_d{x,y}(a,b) | b=r & s=a }	//one free(a)


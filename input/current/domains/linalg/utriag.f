package domains.linalg;

import domains.two_d;

domain utriag{x,y} = { two_d{x,y}(a,b) | b>=a+1 & x=y }


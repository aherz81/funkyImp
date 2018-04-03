package domains.linalg;

import domains.two_d;

domain ltriag{x,y} = { two_d{x,y}(a,b) | b<a & x=y }


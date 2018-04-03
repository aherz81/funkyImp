void function(int i, int j) {
	float *M = (float *) malloc(3*4*sizeof(float));
	//... Init values ...	
	float v = M[i*3+j];	
	free(M);	
}



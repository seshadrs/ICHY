function exitcode = getIdct(fname, oname) 
% fname is the file with the input matrix 
% oname is the file to which the output matrix will be written
	M = dlmread(fname);
	X = idct(M);
	X = X.';
	dlmwrite(oname, X, '\t');
	exitcode = 0;
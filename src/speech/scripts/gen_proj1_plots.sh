#!/bin/bash

WAV_FILE="../../../output/rec.wav"	#the audio rec file
LOGMEL_MAT_40="../../../output/logmel_40.mat"		#the logMel filter spectrum 
IDCT_MAT_40="../../../output/idct_40.mat"	#the inverse DCT of truncated spectrum
LOGMEL_MAT_30="../../../output/logmel_30.mat"		#the logMel filter spectrum 
IDCT_MAT_30="../../../output/idct_30.mat"	#the inverse DCT of truncated spectrum
LOGMEL_MAT_25="../../../output/logmel_25.mat"		#the logMel filter spectrum 
IDCT_MAT_25="../../../output/idct_25.mat"	#the inverse DCT of truncated spectrum

python plt_wavform.py $WAV_FILE
python plt_matrix.py $LOGMEL_MAT_40
python plt_matrix.py $IDCT_MAT_40
python plt_matrix.py $LOGMEL_MAT_30
python plt_matrix.py $IDCT_MAT_30
python plt_matrix.py $LOGMEL_MAT_25
python plt_matrix.py $IDCT_MAT_25

open ../../../output/rec.wav_plot.png ../../../output/logmel_40.mat_plot.png ../../../output/idct_40.mat_plot.png
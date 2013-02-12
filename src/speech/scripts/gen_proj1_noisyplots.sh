#!/bin/bash

WAV_FILE="../../../output/noisyrec.wav"	#the audio rec file
LOGMEL_MAT_40="../../../output/noisylogmel_40.mat"		#the logMel filter spectrum 
IDCT_MAT_40="../../../output/noisyidct_40.mat"	#the inverse DCT of truncated spectrum
LOGMEL_MAT_30="../../../output/noisylogmel_30.mat"		#the logMel filter spectrum 
IDCT_MAT_30="../../../output/noisyidct_30.mat"	#the inverse DCT of truncated spectrum
LOGMEL_MAT_25="../../../output/noisylogmel_25.mat"		#the logMel filter spectrum 
IDCT_MAT_25="../../../output/noisyidct_25.mat"	#the inverse DCT of truncated spectrum

python plt_wavform.py $WAV_FILE
python plt_matrix.py $LOGMEL_MAT_40
python plt_matrix.py $IDCT_MAT_40
python plt_matrix.py $LOGMEL_MAT_30
python plt_matrix.py $IDCT_MAT_30
python plt_matrix.py $LOGMEL_MAT_25
python plt_matrix.py $IDCT_MAT_25

open ../../../output/noisyrec.wav_plot.png ../../../output/noisylogmel_40.mat_plot.png ../../../output/noisyidct_40.mat_plot.png

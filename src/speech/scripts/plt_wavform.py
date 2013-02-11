import sys
from scipy.io.wavfile import read,write
from pylab import plot,show,subplot,specgram

from matplotlib.pylab import *

from numpy import *


rate,data = read(sys.argv[1]) # reading

subplot(211)
plot(range(len(data)),data)


subplot(212)
# NFFT is the number of data points used in each block for the FFT
# and noverlap is the number of points of overlap between blocks
specgram(data, NFFT=128, noverlap=0) # small window


savefig(sys.argv[1]+"_plot.png",Transparent = True)
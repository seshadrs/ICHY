import sys

from matplotlib.pylab import *

from numpy import *

data = loadtxt(sys.argv[1])

data = swapaxes(data,0,1)

revdata = data[::-1]

matshow(revdata,fignum=100,cmap=cm.rainbow)

savefig(sys.argv[1]+"_plot.png",Transparent = True)
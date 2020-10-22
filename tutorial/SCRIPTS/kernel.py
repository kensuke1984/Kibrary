import numpy as np
import matplotlib.pyplot as plt
import matplotlib as mpl
import os
import subprocess
import cartopy.crs as ccrs
from cartopy.mpl.gridliner import LONGITUDE_FORMATTER, LATITUDE_FORMATTER
import matplotlib.ticker as mticker
import re

mpl.rcParams.update({'font.size': 10})

# input_file = 'KernelTemporalVisual_PcP/031704A/LMQ/PcP/' \
#     + 'PcP_LAMBDA_Z_kernelTemporal_snapshots_t0644.txt'
input_file = 'KernelTemporalVisual_ScS/031704A/LMQ/ScS/' \
    + 'ScS_MU_T_kernelTemporal_snapshots_t01171.txt'
kernel = np.loadtxt(input_file)
t0 = float(re.search('t[0-9]{5}', input_file).group(0)[1:])

lat = kernel[:, 0]
lon = kernel[:, 1]
r = kernel[:, 2]
kernel = kernel[:, 3:]

nlat = len(np.unique(lat))
nlon = len(np.unique(lon))
nr = len(np.unique(r))
nt = kernel.shape[1]

lat_comp = np.sort(np.unique(lat))
lon_comp = np.sort(np.unique(lon))
r_comp = np.sort(np.unique(r))

if os.path.isfile('kernel.npy'):
    kernel_comp = np.load('kernel.npy')
else:
    kernel_comp = np.zeros((nlat, nlon, nr, nt), dtype='float')
    for ilat in range(nlat):
        for ilon in range(nlon):
            for ir in range(nr):
                for i in range(kernel.shape[0]):
                    if (
                        lat[i] == lat_comp[ilat]
                        and lon[i] == lon_comp[ilon]
                        and r[i] == r_comp[ir]):
                        kernel_comp[ilat, ilon, ir, :] = kernel[i, :]
    np.save('kernel.npy', kernel_comp)

# set plot parameters
cmap = plt.get_cmap('seismic')
ker_max = np.abs(kernel_comp).max()
norm = mpl.colors.Normalize(vmin=-ker_max,vmax=ker_max)
extent = (
    lon_comp.min(), lon_comp.max(),
    lat_comp.min(), lat_comp.max())
proj = ccrs.PlateCarree()

# plot temporary kernel figs using cartopy
files = []
for it in range(0, nt, 1):
    fig, axes = plt.subplots(
        2, 4, subplot_kw=dict(projection=proj),
        figsize=(15,9))
    for ia, ax in enumerate(axes.ravel()):
        ax.set_extent(extent)
        ax.pcolormesh(
            lon_comp,
            lat_comp,
            kernel_comp[:,:,ia,it],
            cmap=cmap,
            norm=norm,
            transform=proj)
        ax.coastlines()
        gl = ax.gridlines(
            crs=proj, draw_labels=True,
            linewidth=1., color='black', linestyle='--')
        gl.top_labels = False
        gl.right_labels = False
        gl.xlines = False
        gl.ylines = False
        gl.xformatter = LONGITUDE_FORMATTER
        gl.yformatter = LATITUDE_FORMATTER
        ax.set_title(
            'r={} km; t={} s'.format(r_comp[ia], t0+it))
    fname = '_tmp{:03d}.png'.format(it)
    plt.savefig(fname, bbox_inches='tight')
    files.append(fname)
    plt.close()

# make movie
print('Making movie kernel.mpg - this may take a while')
subprocess.call("mencoder 'mf://_tmp*.png' -mf type=png:fps=10 -ovc lavc "
                "-lavcopts vcodec=wmv2 -oac copy -o kernel.mpg", shell=True)

# clean up
for fname in files:
    os.remove(fname)
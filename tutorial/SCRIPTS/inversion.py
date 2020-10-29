import numpy as np
import matplotlib.pyplot as plt
import matplotlib as mpl
import os
import subprocess
import cartopy.crs as ccrs
from cartopy.mpl.gridliner import LONGITUDE_FORMATTER, LATITUDE_FORMATTER
import matplotlib.ticker as mticker
import re
import sys

mpl.rcParams.update({'font.size': 10})

n_cg = int(sys.argv[1])
input_file = 'lmi_ScS/CG/velocityCG{}.txt'.format(n_cg)
inv_model = np.loadtxt(input_file)

lat = inv_model[:, 0]
lon = inv_model[:, 1]
r = inv_model[:, 2]
dv = inv_model[:, 3:]

dv[np.isnan(dv)] = 0.

nlat = len(np.unique(lat))
nlon = len(np.unique(lon))
nr = len(np.unique(r))
ntype = dv.shape[1]

lat_comp = np.sort(np.unique(lat))
lon_comp = np.sort(np.unique(lon))
r_comp = np.sort(np.unique(r))

if os.path.isfile('dv.npy'):
    dv_comp = np.load('dv.npy')
else:
    dv_comp = np.zeros((nlat, nlon, nr, ntype), dtype='float')
    for ilat in range(nlat):
        for ilon in range(nlon):
            for ir in range(nr):
                for i in range(dv.shape[0]):
                    if (
                        lat[i] == lat_comp[ilat]
                        and lon[i] == lon_comp[ilon]
                        and r[i] == r_comp[ir]):
                        dv_comp[ilat, ilon, ir, :] = dv[i, :]
    np.save('dv.npy', dv_comp)

# set plot parameters
cmap = plt.get_cmap('seismic')
dv_max = np.abs(dv).max(axis=0)
extent = (
    lon_comp.min(), lon_comp.max(),
    lat_comp.min(), lat_comp.max())
proj = ccrs.PlateCarree()

print('dv maximum (abs) value: {}'.format(dv_max))

for itype in range(ntype):
    norm = mpl.colors.Normalize(vmin=-dv_max[itype],vmax=dv_max[itype])
    fig, axes = plt.subplots(
        2, 4, subplot_kw=dict(projection=proj),
        figsize=(15,9))
    for ia, ax in enumerate(axes.ravel()):
        ax.set_extent(extent)
        ax.pcolormesh(
            lon_comp,
            lat_comp,
            dv_comp[:,:,ia,itype],
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
            'r={} km'.format(r_comp[ia]))
    fname = 'inversion_model_cg{}_{}.pdf'.format(n_cg, itype)
    plt.savefig(fname, bbox_inches='tight')
    plt.close(fig)

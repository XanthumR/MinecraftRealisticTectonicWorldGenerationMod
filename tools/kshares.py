import numpy as np
exec(open('tools/kregion.py', encoding='utf-8').read().split("regions =")[0])
print(f"real BW {100*np.isin(real,[4,5])[land].mean():.1f}% BS {100*np.isin(real,[6,7])[land].mean():.1f}% | sim BW {100*np.isin(sim,[4,5])[land].mean():.1f}% BS {100*np.isin(sim,[6,7])[land].mean():.1f}%")
for n,(a,b,c,d) in {'Australia':(-38,-12,113,154),'S Africa':(-35,-15,12,40),'Mexico/SW US':(20,37,-118,-95),'Arabia':(12,32,35,60),'Sahara':(16,32,-15,35)}.items():
    m = land&(LAT>=a)&(LAT<=b)&(LON>=c)&(LON<=d)
    print(n, f"BW sim {100*np.isin(sim,[4,5])[m].mean():.0f}% real {100*np.isin(real,[4,5])[m].mean():.0f}%")

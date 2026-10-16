p = 'tools/koppen.py'
s = open(p, encoding='utf-8').read()
a = "    print('\\nmain groups, rows real, columns simulated (share of real land cells):')"
assert s.count(a) == 1, a
s = s.replace(a, """    regions = {'India': (8, 32, 68, 90), 'Sahara': (16, 32, -15, 35), 'Amazon': (-15, 5, -75, -45), 'Eastern US': (28, 45, -95, -70),
               'Siberia': (50, 70, 60, 140), 'Australia': (-38, -12, 113, 154), 'Europe': (36, 60, -10, 30), 'East China': (22, 40, 105, 122)}
    LAT, LON = np.meshgrid(lat, lon, indexing='ij')
    print('main-group agreement by region: ' + ', '.join(
        f"{n} {100 * (group[sim] == group[real])[land & (LAT >= b[0]) & (LAT <= b[1]) & (LON >= b[2]) & (LON <= b[3])].mean():.0f}%" for n, b in regions.items()))
""" + a)
open(p, 'w', encoding='utf-8').write(s)
print("ok")

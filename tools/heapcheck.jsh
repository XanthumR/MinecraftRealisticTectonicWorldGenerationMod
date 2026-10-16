import java.util.*;
    class LongHeap {
        private long[] a;
        private int size;

        LongHeap(int capacity) {
            a = new long[capacity];
        }

        boolean isEmpty() {
            return size == 0;
        }

        void add(long v) {
            if (size == a.length) a = Arrays.copyOf(a, size * 2);
            int i = size++;
            while (i > 0 && a[(i - 1) / 2] > v) {
                a[i] = a[(i - 1) / 2];
                i = (i - 1) / 2;
            }
            a[i] = v;
        }

        long poll() {
            long top = a[0], last = a[--size];
            int i = 0;
            while (2 * i + 1 < size) {
                int c = 2 * i + 1;
                if (c + 1 < size && a[c + 1] < a[c]) c++;
                if (a[c] >= last) break;
                a[i] = a[c];
                i = c;
            }
            a[i] = last;
            return top;
        }
    }


var r = new Random(1); var pq = new PriorityQueue<Long>(); var lh = new LongHeap(16); var seen = new HashSet<Long>(); boolean same = true;
for (int round = 0; round < 1000000; round++) {
  if (r.nextInt(3) > 0 || pq.isEmpty()) { long v = r.nextLong() >>> 8; if (seen.add(v)) { pq.add(v); lh.add(v); } }
  else same &= pq.poll() == lh.poll();
}
while (!pq.isEmpty()) same &= pq.poll() == lh.poll();
System.out.println("heap matches PriorityQueue: " + (same && lh.isEmpty()));
/exit

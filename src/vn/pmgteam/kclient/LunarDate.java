package vn.pmgteam.kclient;

public class LunarDate {
        public int day;
        public int month;
        public int year;
        public boolean leap;
        public int jd;

        public LunarDate(int day, int month, int year, boolean leap, int jd) {
            this.day = day;
            this.month = month;
            this.year = year;
            this.leap = leap;
            this.jd = jd;
        }

        @Override
        public String toString() {
            return "LunarDate{" +
                    "day=" + day +
                    ", month=" + month +
                    ", year=" + year +
                    ", leap=" + leap +
                    ", jd=" + jd +
                    '}';
        }
    }

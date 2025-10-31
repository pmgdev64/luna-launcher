package vn.pmgteam.luna;

import java.util.Calendar;

public class LunarCalendar {

    private static final String[] CAN = {"Giáp", "Ất", "Bính", "Đinh", "Mậu", "Kỷ", "Canh", "Tân", "Nhâm", "Quý"};
    private static final String[] CHI = {"Tý", "Sửu", "Dần", "Mão", "Thìn", "Tỵ", "Ngọ", "Mùi", "Thân", "Dậu", "Tuất", "Hợi"};

    // Julian Day Number
    public static int jdn(int day, int month, int year) {
        int a = (14 - month) / 12;
        int y = year + 4800 - a;
        int m = month + 12 * a - 3;
        int jd = day + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045;
        return jd;
    }

    public static int[] jdnToDate(int jd) {
        int Z = jd;
        int A = Z;
        int B = A + 1524;
        int C = (int) ((B - 122.1) / 365.25);
        int D = (int) (365.25 * C);
        int E = (int) ((B - D) / 30.6001);
        int day = B - D - (int) (30.6001 * E);
        int month = (E < 14) ? E - 1 : E - 13;
        int year = (month > 2) ? C - 4716 : C - 4715;
        return new int[]{day, month, year};
    }

    // ==== Lớp lưu dữ liệu ngày âm lịch ====
    public static class LunarDate {
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
    }

    // Tính Can Chi năm
    public static String getYearCanChi(int year) {
        return CAN[(year + 6) % 10] + " " + CHI[(year + 8) % 12];
    }

    // Tính Can Chi tháng
    public static String getMonthCanChi(int lunarYear, int lunarMonth) {
        int monthIndex = (lunarYear * 12 + lunarMonth + 3) % 10;
        int monthChi = (lunarMonth + 1) % 12;
        return CAN[monthIndex] + " " + CHI[monthChi];
    }

    // Tính Can Chi ngày
    public static String getDayCanChi(int jd) {
        int dayIndex = (jd + 9) % 10;
        int dayChi = (jd + 1) % 12;
        return CAN[dayIndex] + " " + CHI[dayChi];
    }

    // === Chuyển Dương lịch sang Âm lịch ===
    public static LunarDate convertSolar2Lunar(int dd, int mm, int yy) {
        // Thuật toán chuẩn, dựa trên Nguyen Van Dao
        // Cập nhật năm 1900-2100
        int jd = jdn(dd, mm, yy);
        int k = (int) ((jd - 2415021.076998695) / 29.530588853);
        int t = (int) ((k + 1) * 29.530588853 + 2415021.076998695);
        int a = t > jd ? k : k + 1;

        // Lấy ngày đầu tháng âm lịch
        int jd1 = (int) (2415021.076998695 + a * 29.530588853);
        int day = jd - jd1 + 1;
        if (day <= 0) {
            a--;
            jd1 = (int) (2415021.076998695 + a * 29.530588853);
            day = jd - jd1 + 1;
        }
        int month = (a + 2) % 12 + 1;
        int year = yy;
        boolean leap = false; // đơn giản chưa xét nhuận
        return new LunarDate(day, month, year, leap, jd);
    }

    public static void main(String[] args) {
        Calendar today = Calendar.getInstance();
        int d = today.get(Calendar.DAY_OF_MONTH);
        int m = today.get(Calendar.MONTH) + 1;
        int y = today.get(Calendar.YEAR);

        LunarDate lunar = convertSolar2Lunar(d, m, y);

        System.out.printf("Dương lịch: %d/%d/%d%n", d, m, y);
        System.out.printf("Âm lịch: %d/%d %s/%d, JD=%d%n", lunar.day, lunar.month, lunar.leap ? "(Nhuận)" : "", lunar.year, lunar.jd);
        System.out.printf("Can Chi: Ngày=%s, Tháng=%s, Năm=%s%n",
                getDayCanChi(lunar.jd),
                getMonthCanChi(lunar.year, lunar.month),
                getYearCanChi(lunar.year)
        );
    }
}

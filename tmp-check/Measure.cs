using System;
using System.Drawing;
using System.Collections.Generic;
using System.Text;

class Measure
{
    static void Main()
    {
        Bitmap bmp = new Bitmap("tmp-check/after.png");
        int W = bmp.Width, H = bmp.Height;
        Console.WriteLine("image " + W + "x" + H);

        // Find the right-side card: scan columns 1000..1365 for the region with accent-colored pixels.
        // Accent in STANDARD theme is a saturated blue (4C8DFF / 6FA4FF).
        int minX = W, maxX = -1, minY = H, maxY = -1;
        for (int x = 900; x < W; x++)
        {
            for (int y = 0; y < H; y++)
            {
                Color c = bmp.GetPixel(x, y);
                if (c.B > 120 && c.B > c.R + 40 && c.B > c.G + 20)
                {
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }
        Console.WriteLine("accent bbox in right area: L=" + minX + " R=" + maxX + " T=" + minY + " B=" + maxY);

        // Brightness profile per row inside the right area
        int x0 = Math.Max(0, minX - 5), x1 = Math.Min(W - 1, maxX + 5);
        double[] rowLum = new double[H];
        int[] rowCount = new int[H];
        for (int y = 0; y < H; y++)
        {
            double sum = 0; int cnt = 0;
            for (int x = x0; x <= x1; x++)
            {
                Color c = bmp.GetPixel(x, y);
                double lum = 0.2126 * c.R + 0.7152 * c.G + 0.0722 * c.B;
                sum += lum; cnt++;
            }
            rowLum[y] = cnt > 0 ? sum / cnt : 0;
            rowCount[y] = cnt;
        }

        // average luminance of the region between buttons for reference
        var sb = new StringBuilder();
        sb.AppendLine();
        for (int y = 0; y < H; y++)
        {
            if (rowLum[y] > 0)
                sb.AppendLine("y=" + y + " lum=" + rowLum[y].ToString("F1"));
        }
        System.IO.File.WriteAllText("tmp-check/rows.txt", sb.ToString());

        // Detect bright rows (outline): lum significantly above local baseline
        List<int> bright = new List<int>();
        for (int y = 1; y < H - 1; y++)
        {
            if (rowLum[y] > 0 && rowLum[y] > 70 && rowLum[y] >= rowLum[y - 1] && rowLum[y] >= rowLum[y + 1])
                bright.Add(y);
        }
        Console.WriteLine("bright rows candidates: " + string.Join(",", bright));

        // group consecutive
        List<int[]> groups = new List<int[]>();
        foreach (int y in bright)
        {
            bool merged = false;
            foreach (int[] g in groups)
            {
                if (Math.Abs(g[g.Length - 1] - y) <= 2) { merged = true; break; }
            }
            if (!merged) groups.Add(new[] { y });
        }
        Console.WriteLine("groups: " + groups.Count);

        // Print outline rows with their luminance
        foreach (int[] g in groups)
            Console.WriteLine("outline y=" + g[0] + " lum=" + rowLum[g[0]].ToString("F1"));
    }
}

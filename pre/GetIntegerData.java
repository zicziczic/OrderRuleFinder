package leveretconey.pre;

import java.io.*;
import java.util.*;

public class GetIntegerData {

    public static List<List<String>> readCSV(String path, boolean hasTitle, String segmentation) throws IOException {
    File csv = new File(path);
    BufferedReader br = new BufferedReader(new FileReader(csv));
    if (hasTitle) br.readLine();

    String lineDta;
    List<List<String>> list = new ArrayList<>();
    int expectedColumns = -1;
    int rowNumber = hasTitle ? 1 : 0;

    while ((lineDta = br.readLine()) != null) {
        rowNumber++;
        if (lineDta.trim().isEmpty()) continue;              // skip blank trailing line(s)

        String[] parts = lineDta.split(segmentation, -1);    // -1 = keep trailing empty fields
        List<String> row = new ArrayList<>(Arrays.asList(parts));

        if (expectedColumns == -1) {
            expectedColumns = row.size();
        } else if (row.size() != expectedColumns) {
            br.close();
            throw new IOException(String.format(
                "Row %d has %d columns, expected %d. Content: %s",
                rowNumber, row.size(), expectedColumns, lineDta));
        }
        list.add(row);
    }
    br.close();
    return list;
    }

    public static void changeToInt(List<List<String>> listList){
        if(listList.size() == 0) return;
        int len = listList.get(0).size();
        for(int i = 0; i < len; i++){
            int nowSort = i;
            listList.sort(new Comparator<List<String>>() {
                @Override
                public int compare(List<String> o1, List<String> o2) {
                    List<String> list1 = new ArrayList<>(o1);
                    List<String> list2 = new ArrayList<>(o2);
                    return list1.get(nowSort).compareTo(list2.get(nowSort));
                }
            });

            String temp = listList.get(0).get(i);
            int index = 0;

            for (int j = 0; j < listList.size(); j++) {
                if(temp.equals(listList.get(j).get(i))){
                    listList.get(j).set(i, String.valueOf(index));
                }else {
                    index++;
                    temp = listList.get(j).get(i);
                    listList.get(j).set(i, String.valueOf(index));
                }
            }
        }
    }

    // NEW: capture the header instead of throwing it away
    public static List<String> readHeader(String path, String segmentation) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String firstLine = br.readLine();
            if (firstLine == null) return new ArrayList<>();
            return new ArrayList<>(Arrays.asList(firstLine.split(segmentation, -1)));
        }
    }

    // writeCSV now takes the real header, and OVERWRITES instead of appending (fixes the old footgun too)
    public static void writeCSV(List<List<String>> listList, String path, List<String> header) throws IOException {
        File csv2 = new File(path);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(csv2, false))) {   // false = overwrite
            bw.write(String.join(",", header));
            bw.newLine();
            for (List<String> row : listList) {
                bw.write(String.join(",", row));
                bw.newLine();
            }
        }
    }

    public static void main(String[] args) throws IOException {
    String inputPath = "data/data-sampled/diabetes_prediction_dataset_binned_age.csv";
    String outputPath = "data/data-sampled-int/diabetes_prediction_dataset_binned_age.csv";

    List<String> header = readHeader(inputPath, ",");
    List<List<String>> listList = readCSV(inputPath, true, ",");
    changeToInt(listList);
    writeCSV(listList, outputPath, header);
}
}

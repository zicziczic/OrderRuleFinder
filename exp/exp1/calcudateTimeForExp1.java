package leveretconey.exp.exp1;

import leveretconey.cocoa.multipleStandard.DFSDiscovererWithMultipleStandard;
import leveretconey.cocoa.ranking.LODRFClassSquareSumOverAttriCount;
import leveretconey.cocoa.ranking.LODRankingFunction;
import leveretconey.dependencyDiscover.Data.DataFrame;
import leveretconey.dependencyDiscover.Dependency.LexicographicalOrderDependency;
import leveretconey.dependencyDiscover.Discoverer.ALODDiscoverer;
import leveretconey.dependencyDiscover.SortedPartition.ImprovedTwoSideSortedPartition;
import leveretconey.dependencyDiscover.Validator.Result.ValidationResultWithAccurateBound;

import java.io.*;
import java.math.BigDecimal;
import java.util.Collection;

import static leveretconey.cocoa.multipleStandard.DFSDiscovererWithMultipleStandard.ValidatorType.*;


public class calcudateTimeForExp1 {
    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            System.out.println("Usage: calcudateTimeForExp1 <inputCsvPath> <outputFolder> <errorThreshold>");
            System.out.println("Example: calcudateTimeForExp1 data/mydata/mydata-int.csv data/mydata 0.01");
            return;
        }

        String inputPath   = args[0];
        String outputFolder = args[1];
        double threshold    = Double.parseDouble(args[2]);

        // build output path: {outputFolder}/{inputFileNameWithoutExt}_results.csv
        File inFile = new File(inputPath);
        String baseName = inFile.getName();
        int dot = baseName.lastIndexOf('.');
        if (dot > 0) baseName = baseName.substring(0, dot);

        File outDir = new File(outputFolder);
        if (!outDir.exists()) outDir.mkdirs();          // create output folder if missing
        File outFile = new File(outDir, baseName + "_results.csv");

        DataFrame data = DataFrame.fromCsv(inputPath);

        ALODDiscoverer discoverer = new DFSDiscovererWithMultipleStandard(G1, threshold);
        Collection<LexicographicalOrderDependency> aods = discoverer.discover(data, threshold);
        LODRankingFunction rankingFunction = new LODRFClassSquareSumOverAttriCount();

        System.out.println("Discovered " + aods.size() + " candidate rules. Validating and writing as we go...");

        int written = 0;
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(outFile), "UTF-8"))) {

            writer.write("Rule,ErrorRate,RankMeasure");
            writer.newLine();
            writer.flush();

            for (LexicographicalOrderDependency oad : aods) {
                try {
                    LexicographicalOrderDependency od = LexicographicalOrderDependency.fromString(oad.toString());
                    ImprovedTwoSideSortedPartition isp = new ImprovedTwoSideSortedPartition(data, od);
                    ValidationResultWithAccurateBound er = isp.validateForALODWithG1();
                    oad.rank_measure = new BigDecimal(String.valueOf(rankingFunction.getRanking(od, isp)));
                    oad.error_rate   = new BigDecimal(String.valueOf(er.errorRate));

                    //writer.write('"' + oad.toArrowString() + '"' + "," + oad.error_rate + "," + oad.rank_measure);
                    writer.write('"' + oad.toArrowString(data.getColumnNames()) + '"' + "," + oad.error_rate + "," + oad.rank_measure);
                    writer.newLine();
                    writer.flush();          // <-- row hits disk immediately, survives a later crash
                    written++;
                } catch (Exception singleRuleError) {
                    // one bad rule shouldn't kill the whole run
                    System.out.println("Skipped one rule due to error: " + singleRuleError.getMessage());
                }
            }
        }

        System.out.println("Done. Wrote " + written + " / " + aods.size() + " rules to " + outFile.getPath());
    }
}
package me.nettee.astcomparator;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class Main
{
    public static ArrayList<String> PATHS = new ArrayList<String>();
    public static ArrayList<String> PATHS_ = new ArrayList<String>();

    private static double getSimilarity(String path1, String path2)
    {
        Ast ast1 = Ast.fromFile(path1);
        Ast ast2 = Ast.fromFile(path2);
        double similarity = ast1.similarityTo(ast2);
        return similarity;
    }

    public static void main(String[] args)
    {
//        traverseFolder("E:\\eclipse\\ASTTEST\\deeplearning4j-0.0.3",PATHS);
//        traverseFolder("E:\\eclipse\\ASTTEST\\deeplearning4j-0.0.0.1",PATHS_);
        traverseFolder(".\\Data\\weka-1.6.8",PATHS);
        traverseFolder(".\\Data\\weka-1.6.9",PATHS_);
        double AverageSimilarity = 0.0;
        int AllFilesCount = 0;
        for (String e1 : PATHS)
        {
            for (String e2 : PATHS_)
            {
                String[] name1 = e1.split("\\\\");
                String[] name2 = e2.split("\\\\");
                if(name1[name1.length-1].equals(name2[name2.length-1]) && name1[name1.length-1].contains(".java"))
                {
                    double similarity = getSimilarity(e1, e2);
                    AverageSimilarity += similarity;
                    AllFilesCount++;
                    if(similarity < 1.0)
                        System.out.println(name1[name1.length-1] + " " + similarity);
//                    if((AllFilesCount + 1)/100 > AllFilesCount/100)
//                        System.out.println((AllFilesCount+1)/100);
                    break;
                }
            }
        }
        System.out.println(AverageSimilarity/Math.max(PATHS.size(),PATHS_.size()));
    }


    public static void traverseFolder(String path, ArrayList<String> paths)
    {

        File file = new File(path);
        if (file.exists())
        {
            File[] files = file.listFiles();
            if (files.length == 0)
            {
                System.out.println("文件夹是空的!");
                return;
            } else
            {
                for (File file2 : files)
                {
                    if (file2.isDirectory())
                    {
                        //System.out.println("文件夹:" + file2.getAbsolutePath());
                        traverseFolder(file2.getAbsolutePath(),paths);
                    } else
                    {
                        //System.out.println("文件:" + file2.getAbsolutePath());
                        if(file2.getAbsolutePath().contains(".java"))
                            paths.add(file2.getAbsolutePath());
                    }
                }
            }
        } else
        {
            System.out.println("文件不存在!");
        }
    }


}

/*
 * Copyright (c) 2019 Joel Therrien.
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ca.joeltherrien.randomforest.utils;

import ca.joeltherrien.randomforest.tree.Forest;
import ca.joeltherrien.randomforest.tree.ResponseCombiner;
import ca.joeltherrien.randomforest.tree.Tree;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class DataUtils {

    public static <O, FO> Forest<O, FO> loadForest(File folder, ResponseCombiner<O, FO> treeResponseCombiner) throws IOException, ClassNotFoundException {
        if(!folder.isDirectory()){
            throw new IllegalArgumentException("Tree directory must be a directory!");
        }

        final File[] treeFiles = folder.listFiles((file, s) -> s.endsWith(".tree"));
        final List<File> treeFileList = Arrays.asList(treeFiles);

        Collections.sort(treeFileList, Comparator.comparing(File::getName));

        final List<Tree<O>> treeList = new ArrayList<>(treeFileList.size());

        for(final File treeFile : treeFileList){
            final ObjectInputStream inputStream = new ObjectInputStream(new GZIPInputStream(new FileInputStream(treeFile)));

            final Tree<O> tree = (Tree) inputStream.readObject();

            treeList.add(tree);

        }

        return Forest.<O, FO>builder()
                .trees(treeList)
                .treeResponseCombiner(treeResponseCombiner)
                .build();

    }

    public static <O, FO> Forest<O, FO> loadForest(String folder, ResponseCombiner<O, FO> treeResponseCombiner) throws IOException, ClassNotFoundException {
        final File directory = new File(folder);
        return loadForest(directory, treeResponseCombiner);
    }

    public static void saveObject(Serializable object, String filename) throws IOException {
        final ObjectOutputStream outputStream = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(filename)));
        outputStream.writeObject(object);
        outputStream.close();
    }

    public static Object loadObject(String filename) throws IOException, ClassNotFoundException {
        final ObjectInputStream inputStream = new ObjectInputStream(new GZIPInputStream(new FileInputStream(filename)));
        final Object object = inputStream.readObject();
        inputStream.close();

        return object;

    }


}

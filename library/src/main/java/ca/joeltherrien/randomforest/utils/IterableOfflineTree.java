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

import ca.joeltherrien.randomforest.tree.Tree;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;

@RequiredArgsConstructor
public class IterableOfflineTree<Y> implements Iterable<Tree<Y>> {

    private final File[] treeFiles;

    @Override
    public Iterator<Tree<Y>> iterator() {
        return new OfflineTreeIterator<>(treeFiles);
    }

    @RequiredArgsConstructor
    public static class OfflineTreeIterator<Y> implements Iterator<Tree<Y>>{
        private final File[] treeFiles;
        private int position = 0;

        @Override
        public boolean hasNext() {
            return position < treeFiles.length;
        }

        @Override
        public Tree<Y> next() {
            final File treeFile = treeFiles[position];
            position++;


            try {
                final ObjectInputStream inputStream= new ObjectInputStream(new GZIPInputStream(new FileInputStream(treeFile)));
                final Tree<Y> tree = (Tree) inputStream.readObject();
                return tree;
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                throw new RuntimeException("Failed to load tree for " + treeFile.toString());
            }

        }
    }


}

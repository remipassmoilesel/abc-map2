package org.abcmap.core.tileanalyser;

import org.abcmap.core.project.dao.AbstractOrmDAO;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Store interest point of images
 */
public class InterestPointStorage extends AbstractOrmDAO{

    public InterestPointStorage(Path database) throws IOException {
        super(database, InterestPointContainer.class);
    }

}
/*
 * Copyright (c) 2019, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.exceptions;

import tectonicus.Version;

import java.text.MessageFormat;

public class IncompatibleVersionException extends RuntimeException{

    public IncompatibleVersionException(String message){
        super(message);
    }

    public IncompatibleVersionException(Version textureVersion, String worldVersion){
        this(MessageFormat.format("The texture pack version ({0}) is incompatible with the world version ({1}).", textureVersion.getStrVersion(), worldVersion));
    }

}
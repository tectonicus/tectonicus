/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.texture;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import tectonicus.util.FileUtils;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VersionJson {
	private String id;
	private String name;
	@JsonProperty("release_target")
	private String releaseTarget;
	@JsonProperty("world_version")
	private long dataVersion;
	private PackVersion packVersion;
	private boolean stable;

	@JsonProperty("pack_version")
	public void setPackVersion(Object version) {
		if (version instanceof Integer) {
			packVersion = new PackVersion((int) version, 0);
		} else {
			packVersion = FileUtils.getOBJECT_MAPPER().convertValue(version, PackVersion.class);
		}
	}
}

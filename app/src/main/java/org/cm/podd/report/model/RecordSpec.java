package org.cm.podd.report.model;

import java.io.Serializable;

/**
 * Created by pphetra on 21/8/2018 AD.
 */

public class RecordSpec implements Serializable {

    public long id;
    public String name;
    public String tplHeader;
    public String tplSubHeader;
    public long typeId;
    public long timestamp;
    public long parentId;
    public String groupKey;

}

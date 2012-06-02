package com.maintainer.data.provider.hibernate;

import org.apache.commons.lang3.text.WordUtils;
import org.hibernate.annotations.common.util.StringHelper;
import org.hibernate.cfg.ImprovedNamingStrategy;

/**
 * Simple naming strategy to capital-case column names.
 */
public class CapitalNamingStrategy extends ImprovedNamingStrategy {
    private static final long serialVersionUID = 6170159089185151514L;

    @Override
    public String classToTableName(String className) {
        return StringHelper.unqualify(className);
    }

    @Override
    public String propertyToColumnName(String s) {
        return WordUtils.capitalize(s);
    }

    @Override
    public String columnName(String s) {
        return WordUtils.capitalize(s);
    }

    @Override
    public String tableName(String s) {
        return WordUtils.capitalize(s);
    }
}

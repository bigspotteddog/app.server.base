package com.github.bigspotteddog.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;

public class ParameterizedTypeImpl implements ParameterizedType {

  private final Type rawType;
  private final Type[] actualTypeArguments;
  private final Type owner;

  public ParameterizedTypeImpl(Type rawType, Type[] actualTypeArguments, Type owner) {
    this.rawType = rawType;
    this.actualTypeArguments = actualTypeArguments;
    this.owner = owner;
  }

  @Override
  public Type getRawType() {
    return rawType;
  }

  @Override
  public Type[] getActualTypeArguments() {
    return actualTypeArguments;
  }

  @Override
  public Type getOwnerType() {
    return owner;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof ParameterizedType)) {
      return false;
    }
    // Check that information is equivalent
    ParameterizedType that = (ParameterizedType) o;
    if (this == that) {
      return true;
    }
    Type thatOwner = that.getOwnerType();
    Type thatRawType = that.getRawType();

    return (owner == null ? thatOwner == null : owner.equals(thatOwner))
        && (rawType == null ? thatRawType == null : rawType.equals(thatRawType))
        && Arrays.equals(actualTypeArguments, that.getActualTypeArguments());
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(actualTypeArguments)
        ^ (owner == null ? 0 : owner.hashCode())
        ^ (rawType == null ? 0 : rawType.hashCode());
  }
}
// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: DumperServer.proto

package com.alibaba.polardbx.rpc.cdc;

/**
 * Protobuf enum {@code dumper.EventSplitMode}
 */
public enum EventSplitMode
    implements com.google.protobuf.ProtocolMessageEnum {
  /**
   * <pre>
   **
   * Server端对event进行拆包
   * </pre>
   *
   * <code>SERVER = 0;</code>
   */
  SERVER(0),
  /**
   * <pre>
   **
   * Client端对event进行拆包
   * </pre>
   *
   * <code>CLIENT = 1;</code>
   */
  CLIENT(1),
  /**
   * <pre>
   **
   * 随机模式，测试用
   * </pre>
   *
   * <code>RANDOM = 2;</code>
   */
  RANDOM(2),
  UNRECOGNIZED(-1),
  ;

  /**
   * <pre>
   **
   * Server端对event进行拆包
   * </pre>
   *
   * <code>SERVER = 0;</code>
   */
  public static final int SERVER_VALUE = 0;
  /**
   * <pre>
   **
   * Client端对event进行拆包
   * </pre>
   *
   * <code>CLIENT = 1;</code>
   */
  public static final int CLIENT_VALUE = 1;
  /**
   * <pre>
   **
   * 随机模式，测试用
   * </pre>
   *
   * <code>RANDOM = 2;</code>
   */
  public static final int RANDOM_VALUE = 2;


  public final int getNumber() {
    if (this == UNRECOGNIZED) {
      throw new java.lang.IllegalArgumentException(
          "Can't get the number of an unknown enum value.");
    }
    return value;
  }

  /**
   * @param value The numeric wire value of the corresponding enum entry.
   * @return The enum associated with the given numeric wire value.
   * @deprecated Use {@link #forNumber(int)} instead.
   */
  @java.lang.Deprecated
  public static EventSplitMode valueOf(int value) {
    return forNumber(value);
  }

  /**
   * @param value The numeric wire value of the corresponding enum entry.
   * @return The enum associated with the given numeric wire value.
   */
  public static EventSplitMode forNumber(int value) {
    switch (value) {
      case 0: return SERVER;
      case 1: return CLIENT;
      case 2: return RANDOM;
      default: return null;
    }
  }

  public static com.google.protobuf.Internal.EnumLiteMap<EventSplitMode>
      internalGetValueMap() {
    return internalValueMap;
  }
  private static final com.google.protobuf.Internal.EnumLiteMap<
      EventSplitMode> internalValueMap =
        new com.google.protobuf.Internal.EnumLiteMap<EventSplitMode>() {
          public EventSplitMode findValueByNumber(int number) {
            return EventSplitMode.forNumber(number);
          }
        };

  public final com.google.protobuf.Descriptors.EnumValueDescriptor
      getValueDescriptor() {
    if (this == UNRECOGNIZED) {
      throw new java.lang.IllegalStateException(
          "Can't get the descriptor of an unrecognized enum value.");
    }
    return getDescriptor().getValues().get(ordinal());
  }
  public final com.google.protobuf.Descriptors.EnumDescriptor
      getDescriptorForType() {
    return getDescriptor();
  }
  public static final com.google.protobuf.Descriptors.EnumDescriptor
      getDescriptor() {
    return com.alibaba.polardbx.rpc.cdc.DumperServer.getDescriptor().getEnumTypes().get(0);
  }

  private static final EventSplitMode[] VALUES = values();

  public static EventSplitMode valueOf(
      com.google.protobuf.Descriptors.EnumValueDescriptor desc) {
    if (desc.getType() != getDescriptor()) {
      throw new java.lang.IllegalArgumentException(
        "EnumValueDescriptor is not for this type.");
    }
    if (desc.getIndex() == -1) {
      return UNRECOGNIZED;
    }
    return VALUES[desc.getIndex()];
  }

  private final int value;

  private EventSplitMode(int value) {
    this.value = value;
  }

  // @@protoc_insertion_point(enum_scope:dumper.EventSplitMode)
}


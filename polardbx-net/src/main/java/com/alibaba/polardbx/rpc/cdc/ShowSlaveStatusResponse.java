// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: DumperServer.proto

package com.alibaba.polardbx.rpc.cdc;

/**
 * Protobuf type {@code dumper.ShowSlaveStatusResponse}
 */
public final class ShowSlaveStatusResponse extends
    com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:dumper.ShowSlaveStatusResponse)
    ShowSlaveStatusResponseOrBuilder {
private static final long serialVersionUID = 0L;
  // Use ShowSlaveStatusResponse.newBuilder() to construct.
  private ShowSlaveStatusResponse(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private ShowSlaveStatusResponse() {
    response_ = "";
  }

  @java.lang.Override
  @SuppressWarnings({"unused"})
  protected java.lang.Object newInstance(
      UnusedPrivateParameter unused) {
    return new ShowSlaveStatusResponse();
  }

  @java.lang.Override
  public final com.google.protobuf.UnknownFieldSet
  getUnknownFields() {
    return this.unknownFields;
  }
  private ShowSlaveStatusResponse(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    this();
    if (extensionRegistry == null) {
      throw new java.lang.NullPointerException();
    }
    com.google.protobuf.UnknownFieldSet.Builder unknownFields =
        com.google.protobuf.UnknownFieldSet.newBuilder();
    try {
      boolean done = false;
      while (!done) {
        int tag = input.readTag();
        switch (tag) {
          case 0:
            done = true;
            break;
          case 10: {
            java.lang.String s = input.readStringRequireUtf8();

            response_ = s;
            break;
          }
          default: {
            if (!parseUnknownField(
                input, unknownFields, extensionRegistry, tag)) {
              done = true;
            }
            break;
          }
        }
      }
    } catch (com.google.protobuf.InvalidProtocolBufferException e) {
      throw e.setUnfinishedMessage(this);
    } catch (java.io.IOException e) {
      throw new com.google.protobuf.InvalidProtocolBufferException(
          e).setUnfinishedMessage(this);
    } finally {
      this.unknownFields = unknownFields.build();
      makeExtensionsImmutable();
    }
  }
  public static final com.google.protobuf.Descriptors.Descriptor
      getDescriptor() {
    return com.alibaba.polardbx.rpc.cdc.DumperServer.internal_static_dumper_ShowSlaveStatusResponse_descriptor;
  }

  @java.lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return com.alibaba.polardbx.rpc.cdc.DumperServer.internal_static_dumper_ShowSlaveStatusResponse_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            com.alibaba.polardbx.rpc.cdc.ShowSlaveStatusResponse.class, com.alibaba.polardbx.rpc.cdc.ShowSlaveStatusResponse.Builder.class);
  }

  public static final int RESPONSE_FIELD_NUMBER = 1;
  private volatile java.lang.Object response_;
  /**
   * <code>string response = 1;</code>
   * @return The response.
   */
  @java.lang.Override
  public java.lang.String getResponse() {
    java.lang.Object ref = response_;
    if (ref instanceof java.lang.String) {
      return (java.lang.String) ref;
    } else {
      com.google.protobuf.ByteString bs = 
          (com.google.protobuf.ByteString) ref;
      java.lang.String s = bs.toStringUtf8();
      response_ = s;
      return s;
    }
  }
  /**
   * <code>string response = 1;</code>
   * @return The bytes for response.
   */
  @java.lang.Override
  public com.google.protobuf.ByteString
      getResponseBytes() {
    java.lang.Object ref = response_;
    if (ref instanceof java.lang.String) {
      com.google.protobuf.ByteString b = 
          com.google.protobuf.ByteString.copyFromUtf8(
              (java.lang.String) ref);
      response_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  private byte memoizedIsInitialized = -1;
  @java.lang.Override
  public final boolean isInitialized() {
    byte isInitialized = memoizedIsInitialized;
    if (isInitialized == 1) return true;
    if (isInitialized == 0) return false;

    memoizedIsInitialized = 1;
    return true;
  }

  @java.lang.Override
  public void writeTo(com.google.protobuf.CodedOutputStream output)
                      throws java.io.IOException {
    if (!getResponseBytes().isEmpty()) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 1, response_);
    }
    unknownFields.writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    if (!getResponseBytes().isEmpty()) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, response_);
    }
    size += unknownFields.getSerializedSize();
    memoizedSize = size;
    return size;
  }

  @java.lang.Override
  public boolean equals(final java.lang.Object obj) {
    if (obj == this) {
     return true;
    }
    if (!(obj instanceof com.alibaba.polardbx.rpc.cdc.ShowSlaveStatusResponse)) {
      return super.equals(obj);
    }
    com.alibaba.polardbx.rpc.cdc.ShowSlaveStatusResponse other = (com.alibaba.polardbx.rpc.cdc.ShowSlaveStatusResponse) obj;

    if (!getResponse()
        .equals(other.getResponse())) return false;
    if (!unknownFields.equals(other.unknownFields)) return false;
    return true;
  }

  @java.lang.Override
  public int hashCode() {
    if (memoizedHashCode != 0) {
      return memoizedHashCode;
    }
    int hash = 41;
    hash = (19 * hash) + getDescriptor().hashCode();
    hash = (37 * hash) + RESPONSE_FIELD_NUMBER;
    hash = (53 * hash) + getResponse().hashCode();
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static com.alibaba.polardbx.rpc.cdc.ShowSlaveStatusResponse parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static com.alibaba.polardbx.rpc.cdc.ShowSlaveStatusResponse parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static com.alibaba.polardbx.rpc.cdc.ShowSlaveStatusResponse parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static com.alibaba.polardbx.rpc.cdc.ShowSlaveStatusResponse parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static com.alibaba.polardbx.rpc.cdc.ShowSlaveStatusResponse parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static com.alibaba.polardbx.rpc.cdc.ShowSlaveStatusResponse parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static com.alibaba.polardbx.rpc.cdc.ShowSlaveStatusResponse parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static com.alibaba.polardbx.rpc.cdc.ShowSlaveStatusResponse parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }
  public static com.alibaba.polardbx.rpc.cdc.ShowSlaveStatusResponse parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input);
  }
  public static com.alibaba.polardbx.rpc.cdc.ShowSlaveStatusResponse parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static com.alibaba.polardbx.rpc.cdc.ShowSlaveStatusResponse parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static com.alibaba.polardbx.rpc.cdc.ShowSlaveStatusResponse parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }

  @java.lang.Override
  public Builder newBuilderForType() { return newBuilder(); }
  public static Builder newBuilder() {
    return DEFAULT_INSTANCE.toBuilder();
  }
  public static Builder newBuilder(com.alibaba.polardbx.rpc.cdc.ShowSlaveStatusResponse prototype) {
    return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
  }
  @java.lang.Override
  public Builder toBuilder() {
    return this == DEFAULT_INSTANCE
        ? new Builder() : new Builder().mergeFrom(this);
  }

  @java.lang.Override
  protected Builder newBuilderForType(
      com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
    Builder builder = new Builder(parent);
    return builder;
  }
  /**
   * Protobuf type {@code dumper.ShowSlaveStatusResponse}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:dumper.ShowSlaveStatusResponse)
      com.alibaba.polardbx.rpc.cdc.ShowSlaveStatusResponseOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return com.alibaba.polardbx.rpc.cdc.DumperServer.internal_static_dumper_ShowSlaveStatusResponse_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return com.alibaba.polardbx.rpc.cdc.DumperServer.internal_static_dumper_ShowSlaveStatusResponse_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              com.alibaba.polardbx.rpc.cdc.ShowSlaveStatusResponse.class, com.alibaba.polardbx.rpc.cdc.ShowSlaveStatusResponse.Builder.class);
    }

    // Construct using com.alibaba.polardbx.rpc.cdc.ShowSlaveStatusResponse.newBuilder()
    private Builder() {
      maybeForceBuilderInitialization();
    }

    private Builder(
        com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
      super(parent);
      maybeForceBuilderInitialization();
    }
    private void maybeForceBuilderInitialization() {
      if (com.google.protobuf.GeneratedMessageV3
              .alwaysUseFieldBuilders) {
      }
    }
    @java.lang.Override
    public Builder clear() {
      super.clear();
      response_ = "";

      return this;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return com.alibaba.polardbx.rpc.cdc.DumperServer.internal_static_dumper_ShowSlaveStatusResponse_descriptor;
    }

    @java.lang.Override
    public com.alibaba.polardbx.rpc.cdc.ShowSlaveStatusResponse getDefaultInstanceForType() {
      return com.alibaba.polardbx.rpc.cdc.ShowSlaveStatusResponse.getDefaultInstance();
    }

    @java.lang.Override
    public com.alibaba.polardbx.rpc.cdc.ShowSlaveStatusResponse build() {
      com.alibaba.polardbx.rpc.cdc.ShowSlaveStatusResponse result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.lang.Override
    public com.alibaba.polardbx.rpc.cdc.ShowSlaveStatusResponse buildPartial() {
      com.alibaba.polardbx.rpc.cdc.ShowSlaveStatusResponse result = new com.alibaba.polardbx.rpc.cdc.ShowSlaveStatusResponse(this);
      result.response_ = response_;
      onBuilt();
      return result;
    }

    @java.lang.Override
    public Builder clone() {
      return super.clone();
    }
    @java.lang.Override
    public Builder setField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        java.lang.Object value) {
      return super.setField(field, value);
    }
    @java.lang.Override
    public Builder clearField(
        com.google.protobuf.Descriptors.FieldDescriptor field) {
      return super.clearField(field);
    }
    @java.lang.Override
    public Builder clearOneof(
        com.google.protobuf.Descriptors.OneofDescriptor oneof) {
      return super.clearOneof(oneof);
    }
    @java.lang.Override
    public Builder setRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        int index, java.lang.Object value) {
      return super.setRepeatedField(field, index, value);
    }
    @java.lang.Override
    public Builder addRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        java.lang.Object value) {
      return super.addRepeatedField(field, value);
    }
    @java.lang.Override
    public Builder mergeFrom(com.google.protobuf.Message other) {
      if (other instanceof com.alibaba.polardbx.rpc.cdc.ShowSlaveStatusResponse) {
        return mergeFrom((com.alibaba.polardbx.rpc.cdc.ShowSlaveStatusResponse)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(com.alibaba.polardbx.rpc.cdc.ShowSlaveStatusResponse other) {
      if (other == com.alibaba.polardbx.rpc.cdc.ShowSlaveStatusResponse.getDefaultInstance()) return this;
      if (!other.getResponse().isEmpty()) {
        response_ = other.response_;
        onChanged();
      }
      this.mergeUnknownFields(other.unknownFields);
      onChanged();
      return this;
    }

    @java.lang.Override
    public final boolean isInitialized() {
      return true;
    }

    @java.lang.Override
    public Builder mergeFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      com.alibaba.polardbx.rpc.cdc.ShowSlaveStatusResponse parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (com.alibaba.polardbx.rpc.cdc.ShowSlaveStatusResponse) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }

    private java.lang.Object response_ = "";
    /**
     * <code>string response = 1;</code>
     * @return The response.
     */
    public java.lang.String getResponse() {
      java.lang.Object ref = response_;
      if (!(ref instanceof java.lang.String)) {
        com.google.protobuf.ByteString bs =
            (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        response_ = s;
        return s;
      } else {
        return (java.lang.String) ref;
      }
    }
    /**
     * <code>string response = 1;</code>
     * @return The bytes for response.
     */
    public com.google.protobuf.ByteString
        getResponseBytes() {
      java.lang.Object ref = response_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = 
            com.google.protobuf.ByteString.copyFromUtf8(
                (java.lang.String) ref);
        response_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }
    /**
     * <code>string response = 1;</code>
     * @param value The response to set.
     * @return This builder for chaining.
     */
    public Builder setResponse(
        java.lang.String value) {
      if (value == null) {
    throw new NullPointerException();
  }
  
      response_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>string response = 1;</code>
     * @return This builder for chaining.
     */
    public Builder clearResponse() {
      
      response_ = getDefaultInstance().getResponse();
      onChanged();
      return this;
    }
    /**
     * <code>string response = 1;</code>
     * @param value The bytes for response to set.
     * @return This builder for chaining.
     */
    public Builder setResponseBytes(
        com.google.protobuf.ByteString value) {
      if (value == null) {
    throw new NullPointerException();
  }
  checkByteStringIsUtf8(value);
      
      response_ = value;
      onChanged();
      return this;
    }
    @java.lang.Override
    public final Builder setUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.setUnknownFields(unknownFields);
    }

    @java.lang.Override
    public final Builder mergeUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.mergeUnknownFields(unknownFields);
    }


    // @@protoc_insertion_point(builder_scope:dumper.ShowSlaveStatusResponse)
  }

  // @@protoc_insertion_point(class_scope:dumper.ShowSlaveStatusResponse)
  private static final com.alibaba.polardbx.rpc.cdc.ShowSlaveStatusResponse DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new com.alibaba.polardbx.rpc.cdc.ShowSlaveStatusResponse();
  }

  public static com.alibaba.polardbx.rpc.cdc.ShowSlaveStatusResponse getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<ShowSlaveStatusResponse>
      PARSER = new com.google.protobuf.AbstractParser<ShowSlaveStatusResponse>() {
    @java.lang.Override
    public ShowSlaveStatusResponse parsePartialFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return new ShowSlaveStatusResponse(input, extensionRegistry);
    }
  };

  public static com.google.protobuf.Parser<ShowSlaveStatusResponse> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<ShowSlaveStatusResponse> getParserForType() {
    return PARSER;
  }

  @java.lang.Override
  public com.alibaba.polardbx.rpc.cdc.ShowSlaveStatusResponse getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}


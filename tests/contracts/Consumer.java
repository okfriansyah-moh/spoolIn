import com.google.protobuf.FieldMask;
import com.google.protobuf.UnknownFieldSet;
import com.spoolin.core.boundary.graphql.generated.InputValue;
import com.spoolin.core.boundary.graphql.generated.MoviePatch;
import io.grpc.MethodDescriptor;
import java.util.ArrayList;
import java.util.List;
import spoolin.v1.CatalogServiceGrpc;
import spoolin.v1.MediaServiceGrpc;
import spoolin.v1.Spoolin;

/** Compile and execute generated Java consumers; this does not start a transport. */
public final class Consumer {
  public static void main(String[] args) throws Exception {
    var values = Spoolin.MovieValues.newBuilder().setTitle("Exact / title").build();
    var write = Spoolin.MovieWrite.newBuilder()
        .setValues(values)
        .setExpectedVersion(9007199254740993L)
        .setUpdateMask(FieldMask.newBuilder().addPaths("original_title"))
        .build();
    var decoded = Spoolin.MovieWrite.parseFrom(write.toByteArray());
    if (decoded.getExpectedVersion() != 9007199254740993L) throw new AssertionError("Long narrowed");
    if (decoded.getValues().hasOriginalTitle()) throw new AssertionError("Optional absence lost");
    if (!decoded.getUpdateMask().getPathsList().equals(List.of("original_title"))) throw new AssertionError("Mask lost");
    var unknown = write.toBuilder().setUnknownFields(UnknownFieldSet.newBuilder()
        .addField(999, UnknownFieldSet.Field.newBuilder().addVarint(123).build()).build()).build();
    if (!Spoolin.MovieWrite.parseFrom(unknown.toByteArray()).getUnknownFields().hasField(999)) throw new AssertionError("Unknown field lost");
    if (Spoolin.AuditEvent.getDescriptor().findFieldByName("outcome").getNumber() != 8) throw new AssertionError("Audit outcome number");
    if (Spoolin.AuditEvent.getDescriptor().findFieldByName("subject_kind").getNumber() != 9) throw new AssertionError("Audit subject number");
    if (Spoolin.getDescriptor().getServices().size() != 4) throw new AssertionError("Service inventory");
    if (!CatalogServiceGrpc.SERVICE_NAME.equals("spoolin.v1.CatalogService")) throw new AssertionError("Service namespace");
    if (CatalogServiceGrpc.getServiceDescriptor().getMethods().size() != 30) throw new AssertionError("Catalog method inventory");
    if (MediaServiceGrpc.getUploadMethod().getType() != MethodDescriptor.MethodType.CLIENT_STREAMING) throw new AssertionError("Upload stream");
    if (MediaServiceGrpc.getReadAssetMethod().getType() != MethodDescriptor.MethodType.SERVER_STREAMING) throw new AssertionError("Read stream");
    var supplied = new InputValue.Present<String>(null);
    var absent = new InputValue.Absent<String>();
    if (supplied.equals(absent)) throw new AssertionError("Null collapsed into omission");
    var list = new ArrayList<>(List.of("John / Jane", "Doctor / Patient"));
    var present = new InputValue.Present<List<String>>(list);
    list.clear();
    if (!present.value().equals(List.of("John / Jane", "Doctor / Patient"))) throw new AssertionError("Mutable input alias");
    if (!MoviePatch.class.isRecord()) throw new AssertionError("DTO must be a record");
  }
}

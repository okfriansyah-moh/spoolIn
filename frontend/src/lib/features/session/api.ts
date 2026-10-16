import bootstrap from "./bootstrap.graphql?raw";
import type { AccountBootstrapQuery } from "../../generated/graphql";
import { parsePolicy } from "../../policy/parse";
import { createTransport, type GraphqlRequest } from "../../transport/graphql";
import { record, session } from "./parse";
import type { SessionSource } from "./types";
export function createSessionSource(
  request: GraphqlRequest = createTransport(),
): SessionSource {
  return {
    async bootstrap(signal) {
      const input = record(
        await request(bootstrap, {}, { signal, identity: true }),
      );
      const projection: AccountBootstrapQuery["session"] = session(
        input.session,
      );
      return { session: projection, policy: parsePolicy(input.policy) };
    },
  };
}

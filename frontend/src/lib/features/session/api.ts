import bootstrap from './bootstrap.graphql?raw';
import registerDocument from './register.graphql?raw';
import signInDocument from './sign-in.graphql?raw';
import signOutDocument from './sign-out.graphql?raw';
import operationDocument from './operation.graphql?raw';
import type {
  AccountBootstrapQuery,
  AccountRegistrationMutationVariables,
  AccountSignInMutationVariables,
} from '../../generated/graphql';
import { parsePolicy } from '../../policy/parse';
import { createTransport, type GraphqlRequest } from '../../transport/graphql';
import { record, session } from './parse';
import { mutation, operation } from './outcome';
import type { SessionSource } from './types';
import type { AccountCommands, AccountSubmission } from './commands';
class SessionApi implements SessionSource, AccountCommands {
  constructor(private readonly request: GraphqlRequest) {}
  async bootstrap(signal: AbortSignal) {
    const input = record(
      await this.request(bootstrap, {}, { signal, identity: true }),
    );
    const projection: AccountBootstrapQuery['session'] = session(input.session);
    return { session: projection, policy: parsePolicy(input.policy) };
  }
  async submit(
    input: AccountSubmission,
    operationId: string,
    csrf: string,
    signal: AbortSignal,
  ) {
    const options = { mutation: true, csrf, signal };
    if (input.kind === 'register') {
      const variables: AccountRegistrationMutationVariables = {
        operationId,
        input: {
          name: input.name,
          email: input.email,
          password: input.password,
        },
      };
      const data = record(
        await this.request(registerDocument, variables, options),
      );
      return mutation(data.register, operationId);
    }
    const variables: AccountSignInMutationVariables = {
      operationId,
      email: input.email,
      password: input.password,
    };
    const data = record(await this.request(signInDocument, variables, options));
    return mutation(data.signIn, operationId);
  }
  async signOut(operationId: string, csrf: string, signal: AbortSignal) {
    const data = record(
      await this.request(
        signOutDocument,
        { operationId },
        { mutation: true, csrf, signal },
      ),
    );
    return mutation(data.signOut, operationId);
  }
  async operation(operationId: string, csrf: string, signal: AbortSignal) {
    const data = record(
      await this.request(
        operationDocument,
        { id: operationId },
        { csrf, signal },
      ),
    );
    return data.operation === null
      ? null
      : operation(data.operation, operationId);
  }
}
export function createSessionSource(
  request: GraphqlRequest = createTransport(),
): SessionSource & AccountCommands {
  return new SessionApi(request);
}

import React from 'react';
import { Switch } from 'react-router-dom';

import ErrorBoundaryRoute from 'app/shared/error/error-boundary-route';

import Foo from './foo';
import FooDetail from './foo-detail';
import FooUpdate from './foo-update';
import FooDeleteDialog from './foo-delete-dialog';

const Routes = ({ match }) => (
  <>
    <Switch>
      <ErrorBoundaryRoute exact path={`${match.url}/new`} component={FooUpdate} />
      <ErrorBoundaryRoute exact path={`${match.url}/:id/edit`} component={FooUpdate} />
      <ErrorBoundaryRoute exact path={`${match.url}/:id`} component={FooDetail} />
      <ErrorBoundaryRoute path={match.url} component={Foo} />
    </Switch>
    <ErrorBoundaryRoute exact path={`${match.url}/:id/delete`} component={FooDeleteDialog} />
  </>
);

export default Routes;

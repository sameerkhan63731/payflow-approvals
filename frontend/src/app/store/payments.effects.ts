import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { Store } from '@ngrx/store';
import { catchError, exhaustMap, forkJoin, map, of, switchMap, tap, withLatestFrom } from 'rxjs';
import { PaymentsApi, errorMessage } from '../core/payments-api.service';
import { PaymentsActions } from './payments.actions';
import { selectFilter } from './payments.selectors';

@Injectable()
export class PaymentsEffects {
  private readonly actions$ = inject(Actions);
  private readonly api = inject(PaymentsApi);
  private readonly store = inject(Store);
  private readonly router = inject(Router);

  load$ = createEffect(() =>
    this.actions$.pipe(
      ofType(PaymentsActions.load),
      switchMap(({ status }) =>
        forkJoin({ payments: this.api.list(status), summary: this.api.summary() }).pipe(
          map(({ payments, summary }) => PaymentsActions.loadSuccess({ payments, summary })),
          catchError((err: unknown) => of(PaymentsActions.loadFailure({ error: errorMessage(err) }))),
        ),
      ),
    ),
  );

  open$ = createEffect(() =>
    this.actions$.pipe(
      ofType(PaymentsActions.open),
      switchMap(({ id }) =>
        this.api.get(id).pipe(
          map((payment) => PaymentsActions.openSuccess({ payment })),
          catchError((err: unknown) => of(PaymentsActions.openFailure({ error: errorMessage(err) }))),
        ),
      ),
    ),
  );

  // exhaustMap ignores repeat clicks while a decision is in flight.
  decide$ = createEffect(() =>
    this.actions$.pipe(
      ofType(PaymentsActions.decide),
      exhaustMap(({ id, kind, note }) => {
        const call =
          kind === 'approve' ? this.api.approve(id, note) : kind === 'reject' ? this.api.reject(id, note) : this.api.release(id);
        return call.pipe(
          map((payment) => PaymentsActions.decideSuccess({ payment })),
          catchError((err: unknown) => of(PaymentsActions.decideFailure({ error: errorMessage(err) }))),
        );
      }),
    ),
  );

  create$ = createEffect(() =>
    this.actions$.pipe(
      ofType(PaymentsActions.create),
      exhaustMap(({ request, idempotencyKey }) =>
        this.api.create(request, idempotencyKey).pipe(
          map((payment) => PaymentsActions.createSuccess({ payment })),
          catchError((err: unknown) => of(PaymentsActions.createFailure({ error: errorMessage(err) }))),
        ),
      ),
    ),
  );

  /** Keep the dashboard counts honest after anything changes. */
  refreshAfterChange$ = createEffect(() =>
    this.actions$.pipe(
      ofType(PaymentsActions.decideSuccess, PaymentsActions.createSuccess),
      withLatestFrom(this.store.select(selectFilter)),
      map(([, status]) => PaymentsActions.load({ status })),
    ),
  );

  openCreated$ = createEffect(
    () =>
      this.actions$.pipe(
        ofType(PaymentsActions.createSuccess),
        tap(({ payment }) => void this.router.navigate(['/payments', payment.id])),
      ),
    { dispatch: false },
  );
}

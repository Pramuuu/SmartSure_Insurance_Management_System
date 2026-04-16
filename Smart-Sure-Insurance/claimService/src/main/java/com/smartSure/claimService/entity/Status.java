package com.smartSure.claimService.entity;

import com.smartSure.claimService.exception.InvalidStatusTransitionException;

public enum Status {

    DRAFT {
        @Override
        public Status moveTo(Status next) {
            if (next == SUBMITTED) return next;
            throw new InvalidStatusTransitionException(this.name(), next.name());
        }
    },
    SUBMITTED {
        @Override
        public Status moveTo(Status next) {
            if (next == UNDER_REVIEW || next == APPROVED || next == REJECTED) return next;
            throw new InvalidStatusTransitionException(this.name(), next.name());
        }
    },
    UNDER_REVIEW {
        @Override
        public Status moveTo(Status next) {
            if (next == APPROVED || next == REJECTED) return next;
            throw new InvalidStatusTransitionException(this.name(), next.name());
        }
    },
    APPROVED {
        @Override
        public Status moveTo(Status next) {
            if (next == CLOSED) return next;
            throw new InvalidStatusTransitionException(this.name(), next.name());
        }
    },
    REJECTED {
        @Override
        public Status moveTo(Status next) {
            if (next == CLOSED) return next;
            throw new InvalidStatusTransitionException(this.name(), next.name());
        }
    },
    CLOSED {
        @Override
        public Status moveTo(Status next) {
            throw new InvalidStatusTransitionException(this.name(), next.name());
        }
    };

    public abstract Status moveTo(Status next);
}

/*
 * Copyright (C) 2023 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.vaultingest.config.validator;

import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileMustExistValidator implements ConstraintValidator<FileMustExist, Path> {
    @Override
    public boolean isValid(Path path, ConstraintValidatorContext constraintValidatorContext) {
        if (Files.exists(path)) {
            return true;
        }

        var ctx = constraintValidatorContext.unwrap(HibernateConstraintValidatorContext.class);
        ctx.addMessageParameter("path", path.toString());

        return false;
    }
}

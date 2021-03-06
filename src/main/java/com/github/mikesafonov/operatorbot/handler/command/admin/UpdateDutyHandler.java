package com.github.mikesafonov.operatorbot.handler.command.admin;

import com.github.mikesafonov.operatorbot.command.ParsedCommand;
import com.github.mikesafonov.operatorbot.command.Parser;
import com.github.mikesafonov.operatorbot.exceptions.CommandFormatException;
import com.github.mikesafonov.operatorbot.exceptions.UserFormatException;
import com.github.mikesafonov.operatorbot.exceptions.UserNotFoundException;
import com.github.mikesafonov.operatorbot.handler.MessageHandler;
import com.github.mikesafonov.operatorbot.model.User;
import com.github.mikesafonov.operatorbot.service.AuthorizationTelegram;
import com.github.mikesafonov.operatorbot.service.TimetableService;
import com.github.mikesafonov.operatorbot.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@Slf4j
@RequiredArgsConstructor
public class UpdateDutyHandler implements MessageHandler {
    private final TimetableService timetableService;
    private final UserService userService;
    private final Parser parser;

    @Override
    public SendMessage operate(String chatId, AuthorizationTelegram user, ParsedCommand parsedCommand) {
        return getDutyUpdatingMessage(chatId, parsedCommand.getText(), user);
    }

    private SendMessage getDutyUpdatingMessage(String chatId, String message, AuthorizationTelegram user) {
        StringBuilder text = new StringBuilder();
        if(user.isAdmin()) {
            try {
                updateDuty(message);
                text.append("Дежурный успешно обновлен!");
            } catch (UserNotFoundException e) {
                log.error("User with this id not found!", e);
                text.append("Пользователя с таким id не существует!");
            } catch (CommandFormatException e) {
                log.error("Incorrect command format", e);
                text.append("Команда введена неверно!");
            } catch (UserFormatException e) {
                log.error("User can not be duty!");
                text.append("Этот пользователь не может быть дежурным!");
            }
        } else {
            text.append("Команда не доступна!");
        }
        return SendMessage.builder()
                .chatId(chatId)
                .text(text.toString())
                .build();
    }

    private void updateDuty(String message) {
        LocalDate date = getDateFromMessage(message);
        String telegramId = getIdFromMessage(message);
        if(date != null && telegramId != null) {
            if(timetableService.findByDate(date).isPresent()) {
                updateDuty(date, telegramId);
            } else {
                addDuty(date, telegramId);
            }
        } else {
            throw new CommandFormatException("Incorrect command format!");
        }
    }

    private void addDuty(LocalDate date, String telegramId) {
        User user = userService.findByTelegramId(telegramId).orElseThrow(() -> new UserNotFoundException("User not found!"));
        timetableService.addNote(user, date);
    }

    private void updateDuty(LocalDate date, String telegramId) {
        User user = userService.findByTelegramId(telegramId).orElseThrow(() -> new UserNotFoundException("User not found!"));
        timetableService.updateUserDate(date, user);
    }

    private LocalDate getDateFromMessage(String message) {
        try {
            return LocalDate.parse(parser.getParamValue(message, 0, 2));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private String getIdFromMessage(String message) {
        try {
            return parser.getParamValue(message, 1, 2);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
